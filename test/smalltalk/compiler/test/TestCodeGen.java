package smalltalk.compiler.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TestCodeGen extends BaseTest {
	private String fileName;
	private String code;
	private String expecting;

	public TestCodeGen(String fileName, String code, String expecting) {
		this.fileName = fileName;
		this.code = code;
		this.expecting = expecting;
	}

	@Test
	public void testCode() throws Exception {
		String result = compile(fileName, code);
		assertEquals(expecting, result);
	}

	@Parameterized.Parameters(name="{0}")
	public static Collection<Object[]> getAllTestDescriptors() {
		return getAllTestDescriptors("CodeGen");
	}
}
