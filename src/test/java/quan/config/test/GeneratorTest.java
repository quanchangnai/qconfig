package quan.config.test;

import org.junit.jupiter.api.Test;
import quan.config.generator.Generator;

/**
 * @author quanchangnai
 */
public class GeneratorTest {

    public static void main(String[] args) {
        Generator.generate("src/main/resources/quan/config/generator/generator.properties");
    }

    @Test
    public void test1() {
        System.err.println("test1");
    }
}
