package com.bbs.bbs;

import com.bbs.bbs.support.MySqlContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BbsApplicationTests extends MySqlContainerSupport {

	@Test
	void contextLoads() {
	}

}
