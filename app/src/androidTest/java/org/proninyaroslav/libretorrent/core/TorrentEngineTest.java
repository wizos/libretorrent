/*
 * Copyright (C) 2019-2022 Yaroslav Pronin <proninyaroslav@mail.ru>
 *
 * This file is part of LibreTorrent.
 *
 * LibreTorrent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibreTorrent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibreTorrent.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.proninyaroslav.libretorrent.core;

import android.net.Uri;
import android.util.Log;
import androidx.core.util.Pair;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.proninyaroslav.libretorrent.AbstractTest;
import org.proninyaroslav.libretorrent.core.model.TorrentInfoProvider;
import org.proninyaroslav.libretorrent.core.model.data.MagnetInfo;
import org.proninyaroslav.libretorrent.core.model.data.Priority;
import org.proninyaroslav.libretorrent.core.model.TorrentEngineListener;
import org.proninyaroslav.libretorrent.core.model.AddTorrentParams;
import org.proninyaroslav.libretorrent.core.model.data.entity.Torrent;
import org.proninyaroslav.libretorrent.core.exception.TorrentAlreadyExistsException;
import org.proninyaroslav.libretorrent.core.model.data.metainfo.TorrentMetaInfo;
import org.proninyaroslav.libretorrent.core.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;

@RunWith(AndroidJUnit4.class)
public class TorrentEngineTest extends AbstractTest
{
    @SuppressWarnings("unused")
    private static final String TAG = TorrentEngineTest.class.getSimpleName();

    private AddTorrentParams params, params2;
    private String torrentUrl = "https://webtorrent.io/torrents/wired-cd.torrent";
    private String torrentName = "The WIRED CD - Rip. Sample. Mash. Share";
    private String torrentHash = "a88fda5954e89178c372716a6a78b8180ed4dad3";
    private int filesCount = 18;
    private String torrentUrl2 = "https://webtorrent.io/torrents/sintel.torrent";
    private String torrentName2 = "Sintel";
    private String torrentHash2 = "08ada5a7a6183aae1e09d831df6748d566095a10";
    private String magnetHash = "85922fbee6dce5e2f5491e16bcdd9e6e427ba5aa";
    private int filesCount2 = 11;
    private String magnet = "magnet:?xt=urn:btih:QWJC7PXG3TS6F5KJDYLLZXM6NZBHXJNK";
    private Uri dir;

    @Before
    public void init()
    {
        super.init();

        engine.start();

        dir = Uri.parse("file://" + fs.getDefaultDownloadPath());
        var p1 = new Priority[filesCount];
        Arrays.fill(p1, Priority.DEFAULT);
        params = new AddTorrentParams(downloadTorrent(torrentUrl), false,
                torrentHash, torrentName,
                p1, dir,
                false, false, new ArrayList<>(), false);

        var p2 = new Priority[filesCount];
        Arrays.fill(p2, Priority.DEFAULT);
        params2 = new AddTorrentParams(downloadTorrent(torrentUrl2), false,
                torrentHash2, torrentName2,
                p2, dir,
                false, false, new ArrayList<>(), false);
    }

    @Test
    public void downloadTest()
    {
        CountDownLatch c = new CountDownLatch(1);

        assertTrue(engine.isRunning());

        engine.addListener(new TorrentEngineListener() {
            @Override
            public void onTorrentFinished(@NonNull String id)
            {
                if (!params.sha1hash.equals(id))
                    return;

                c.countDown();
                try {
                    Torrent t = torrentRepo.getTorrentById(id);
                    assertNotNull(t);

                    /* Check if file exists */
                    File file = new File(dir.getPath(), torrentName);
                    assertTrue(file.exists());

                } finally {
                    engine.deleteTorrents(Collections.singletonList(id), true);
                }
            }
        });

        try {
            engine.addTorrentSync(params, true);
            c.await();

        } catch (Exception e) {
            fail(Log.getStackTraceString(e));
        }
    }

    @Test
    public void downloadTest_duplicateTorrent()
    {
        CountDownLatch c = new CountDownLatch(1);
        assertTrue(engine.isRunning());

        engine.addListener(new TorrentEngineListener() {
            @Override
            public void onTorrentFinished(@NonNull String id)
            {
                if (!params.sha1hash.equals(id))
                    return;

                c.countDown();
                try {
                    Torrent t = torrentRepo.getTorrentById(id);
                    assertNotNull(t);

                    try {
                        engine.addTorrentSync(params, true);

                    } catch (TorrentAlreadyExistsException e) {
                        engine.deleteTorrents(Collections.singletonList(id), true);
                        return;
                    }
                    fail();

                } catch (Exception e) {
                    engine.deleteTorrents(Collections.singletonList(id), true);
                    fail(Log.getStackTraceString(e));
                }
            }
        });

        try {
            engine.addTorrentSync(params, true);
            c.await();

        } catch (Exception e) {
            fail(Log.getStackTraceString(e));
        }
    }

    @Test
    public void applyParamsTest()
    {
        TorrentInfoProvider infoProvider = TorrentInfoProvider.getInstance(engine, tagRepo);

        CountDownLatch c = new CountDownLatch(1);
        AtomicBoolean applying = new AtomicBoolean();
        boolean sequentialDownload = true;
        String name = "test";

        assertTrue(engine.isRunning());

        engine.addListener(new TorrentEngineListener() {
            @Override
            public void onTorrentAdded(@NonNull String id)
            {
                if (TorrentEngineTest.this.params.sha1hash.equals(id)) {
                    engine.setSequentialDownload(id, sequentialDownload);
                    engine.setTorrentName(id, name);
                    applying.set(true);
                }
            }
        });

        Disposable d = infoProvider.observeInfo(this.params.sha1hash)
                .subscribe((info) -> {
                    c.countDown();
                    assertTrue(applying.get());

                    try {
                        assertNotNull(info);
                        assertEquals(params.name, info.name);

                    } finally {
                        engine.deleteTorrents(Collections.singletonList(info.torrentId), true);
                    }
                });

        try {
            engine.addTorrentSync(this.params, true);
            c.await();

        } catch (Exception e) {
            fail(Log.getStackTraceString(e));

        } finally {
            d.dispose();
        }
    }

    @Test
    public void moveTorrentTest()
    {
        CountDownLatch c = new CountDownLatch(1);
        AtomicBoolean applying = new AtomicBoolean();
        Uri dirPath = Uri.parse("file://" + fs.getUserDirPath());

        assertTrue(engine.isRunning());

        engine.addListener(new TorrentEngineListener() {
            @Override
            public void onTorrentAdded(@NonNull String id)
            {
                if (TorrentEngineTest.this.params.sha1hash.equals(id)) {
                    engine.setDownloadPath(id, dirPath);
                    applying.set(true);
                }
            }

            @Override
            public void onTorrentMoving(@NonNull String id)
            {
                applying.set(true);
            }

            @Override
            public void onTorrentMoved(@NonNull String id, boolean success)
            {
                if (!TorrentEngineTest.this.params.sha1hash.equals(id))
                    return;

                c.countDown();

                assertTrue(success);

                try {
                    Torrent t = torrentRepo.getTorrentById(id);
                    assertNotNull(t);
                    assertEquals(dirPath, t.downloadPath);
                    /* Check if file exists */
                    File file = new File(dir.getPath(), torrentName);
                    assertTrue(file.exists());

                } finally {
                    engine.deleteTorrents(Collections.singletonList(id), true);
                }
            }
        });

        try {
            engine.addTorrentSync(this.params, true);
            c.await();

        } catch (Exception e) {
            fail(Log.getStackTraceString(e));
        }
    }

    @Test
    public void fetchMagnetTest()
    {
        CountDownLatch c = new CountDownLatch(1);
        Disposable d = null;

        assertTrue(engine.isRunning());

        try {
            Pair<MagnetInfo, Single<TorrentMetaInfo>> res = engine.fetchMagnet(magnet);
            MagnetInfo magnetInfo = res.first;

            assertEquals(magnet, magnetInfo.getUri());
            assertEquals(magnetHash, magnetInfo.getSha1hash());

            d = res.second.subscribe((info) -> {
                        c.countDown();
                        assertNotNull(info);
                        assertEquals(magnetInfo.getSha1hash(), info.sha1Hash);
                    });

            c.await();

        } catch (Exception e) {
            fail(Log.getStackTraceString(e));
        } finally {
            if (d != null)
                d.dispose();
        }
    }

    private String downloadTorrent(String url)
    {
        File tmp = fs.makeTempFile(".torrent");
        try {
            byte[] response = Utils.fetchHttpUrl(context, url);
            org.apache.commons.io.FileUtils.writeByteArrayToFile(tmp, response);

        } catch (Exception e) {
            fail(Log.getStackTraceString(e));
        }

        return "file://" + tmp.getAbsolutePath();
    }
}