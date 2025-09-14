package by.rayden.paracoder.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
class RecodeCommandTest {
    @InjectMocks
    private RecodeCommand recodeCommand;

    @SuppressWarnings("MagicNumber")
    @Test
    void getFFMpegMetadataTest() {
        var trackPayload = CueTrackPayload
            .builder()
            .songNumber(8)
            .title("Song tittle")
            .performer("Song performer")
            .album("Song album")
            .year(2001)
            .genre("    ") // should not be included in the metadata
            .comment("Some comment")
            .discId("") // should not be included in the metadata
            .discNumber(null) // should not be included in the metadata
            .totalDiscs(null) // should not be included in the metadata
            .build();

        String metadata = this.recodeCommand.getFFMpegMetadata(trackPayload);

        assertThat(metadata)
            .contains(
                " -metadata ARTIST=\"Song performer\" ",
                " -metadata TITLE=\"Song tittle\" ",
                " -metadata ALBUM=\"Song album\" ",
                " -metadata TRACK=\"08\" ",
                " -metadata DATE=\"2001\" ",
                " -metadata COMMENT=\"Some comment\" "
            )
            .doesNotContain("DISCID", "DISCNUMBER", "GENRE");
    }
}
