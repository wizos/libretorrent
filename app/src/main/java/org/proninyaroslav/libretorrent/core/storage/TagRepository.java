/*
 * Copyright (C) 2020 Yaroslav Pronin <proninyaroslav@mail.ru>
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

package org.proninyaroslav.libretorrent.core.storage;

import androidx.annotation.NonNull;

import org.proninyaroslav.libretorrent.core.model.data.entity.TagInfo;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public interface TagRepository {
    void insert(@NonNull TagInfo info);

    void update(@NonNull TagInfo info);

    void delete(@NonNull TagInfo info);

    TagInfo getByName(@NonNull String name);

    Flowable<List<TagInfo>> observeAll();

    Flowable<List<TagInfo>> observeByTorrentId(String torrentId);

    Single<List<TagInfo>> getByTorrentIdAsync(String torrentId);

    List<TagInfo> getByTorrentId(String torrentId);
}
