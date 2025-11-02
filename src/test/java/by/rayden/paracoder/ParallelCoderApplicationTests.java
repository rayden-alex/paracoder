package by.rayden.paracoder;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE, args = {"--version"})
class ParallelCoderApplicationTests {

	@Test
	void contextLoads() {
	}

}
