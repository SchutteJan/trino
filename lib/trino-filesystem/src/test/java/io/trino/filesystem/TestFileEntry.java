/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.filesystem;

import io.trino.filesystem.FileEntry.BlockLocation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestFileEntry
{
    private static final Instant MODIFIED = Instant.ofEpochSecond(1234567890);

    @Test
    public void testEmptyBlockLocations()
    {
        assertThat(new FileEntry("/test", 123, MODIFIED, Optional.empty()))
                .satisfies(entry -> {
                    assertThat(entry.path()).isEqualTo("/test");
                    assertThat(entry.length()).isEqualTo(123);
                    assertThat(entry.lastModified()).isEqualTo(MODIFIED);
                    assertThat(entry.blockLocations()).isEmpty();
                });
    }

    @Test
    public void testPresentBlockLocations()
    {
        List<BlockLocation> locations = List.of(
                new BlockLocation(List.of(), 0, 50),
                new BlockLocation(List.of(), 50, 70),
                new BlockLocation(List.of(), 100, 150));
        assertThat(new FileEntry("/test", 200, MODIFIED, Optional.of(locations)))
                .satisfies(entry -> assertThat(entry.blockLocations()).contains(locations));
    }

    @Test
    public void testMissingBlockLocations()
    {
        assertThatThrownBy(() -> new FileEntry("/test", 0, MODIFIED, Optional.of(List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockLocations is empty");
    }

    @Test
    public void testBlockLocationsEmptyFile()
    {
        List<BlockLocation> locations = List.of(new BlockLocation(List.of(), 0, 0));
        assertThat(new FileEntry("/test", 0, MODIFIED, Optional.of(locations)))
                .satisfies(entry -> assertThat(entry.blockLocations()).contains(locations));
    }

    @Test
    public void testBlockLocationsGapAtStart()
    {
        List<BlockLocation> locations = List.of(new BlockLocation(List.of(), 50, 50));
        assertThatThrownBy(() -> new FileEntry("/test", 100, MODIFIED, Optional.of(locations)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockLocations has a gap");
    }

    @Test
    public void testBlockLocationsGapInMiddle()
    {
        List<BlockLocation> locations = List.of(
                new BlockLocation(List.of(), 0, 50),
                new BlockLocation(List.of(), 100, 100));
        assertThatThrownBy(() -> new FileEntry("/test", 200, MODIFIED, Optional.of(locations)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockLocations has a gap");
    }

    @Test
    public void testBlockLocationsGapAtEnd()
    {
        List<BlockLocation> locations = List.of(
                new BlockLocation(List.of(), 0, 50),
                new BlockLocation(List.of(), 50, 49));
        assertThatThrownBy(() -> new FileEntry("/test", 100, MODIFIED, Optional.of(locations)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockLocations does not cover file");
    }
}
