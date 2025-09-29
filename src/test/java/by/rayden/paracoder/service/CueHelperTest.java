package by.rayden.paracoder.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CueHelperTest {

    @InjectMocks
    private CueHelper cueHelper;

    @Test
    void getFilteredPathMapTest() {
        BasicFileAttributes fileAttributes = mock(BasicFileAttributes.class);
        when(fileAttributes.isRegularFile()).thenReturn(true);
        BasicFileAttributes dirAttributes = mock(BasicFileAttributes.class);
        when(dirAttributes.isDirectory()).thenReturn(true);

        Path path1 = Path.of("c:\\dir1\\file1.flac");
        Path path2 = Path.of("c:\\dir1\\file2.Cue");
        Path path3 = Path.of("c:\\dir2\\file3.wav");
        Path path4 = Path.of("c:\\dir2\\file4.cUe");
        Path path5 = Path.of("c:\\dir2\\");

        Map<Path, BasicFileAttributes> pathMap = Map.of(
            path1, fileAttributes,
            path2, fileAttributes,
            path3, fileAttributes,
            path4, fileAttributes,
            path5, dirAttributes
        );

        Map<Path, BasicFileAttributes> filteredPathMap = this.cueHelper.getFilteredPathMap(pathMap);

        assertThat(filteredPathMap).containsOnlyKeys(path2, path4, path5);
    }

    @Test
    void readCueSheetTest() throws Exception {
        var cueSheet = this.cueHelper.readCueSheet(Paths.get("src/test/resources/CyrillicUTF8.cue"));

        assertThat(cueSheet.getGenre()).isEqualTo("Pop Rock");
        assertThat(cueSheet.getPerformer()).isEqualTo("Мара");
    }

}
