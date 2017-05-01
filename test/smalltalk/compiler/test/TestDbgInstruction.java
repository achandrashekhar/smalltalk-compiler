package smalltalk.compiler.test;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TestDbgInstruction extends BaseTest {
	private String fileName;
	private String code;
	private String expecting;

	public TestDbgInstruction(String fileName, String code, String expecting) {
		this.fileName = fileName;
		this.code = code;
		this.expecting = expecting;
	}

	@Ignore
	@Test
	public void testCode() throws Exception {
		boolean genDbg = true;
		String result = compile(fileName, code, genDbg);
		assertEquals(expecting, result);
	}

	@Parameterized.Parameters(name="{0}")
	public static Collection<Object[]> getAllTestDescriptors() {
		return getAllTestDescriptors("DbgInstruction");
	}
}
