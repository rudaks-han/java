import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExpTest {

    @Test
    public void testSiteName() {

        /*String regexp = ".*(EER|ECC)_([0-9]\\.[0-9]\\.[0-9]).*";
        String str = "한국토지주택공사_ECC_2.1.2";*/

        String regexp = ".*(ecc|eer)/tags/([0-9]\\.[0-9]\\.[0-9])(.*)";
        String str = "/eer/tags/3.1.1/home/test/script/unix/report.sh";

        Pattern infoPattern = Pattern.compile(regexp);
        Matcher infoMatcher = infoPattern.matcher(str);

        String product = "";
        String eerVersion = "";
        if (infoMatcher.find()) {
            product = infoMatcher.group(1);
            eerVersion = infoMatcher.group(2);
        }

        System.out.println("product : " + product);
        System.out.println("eerVersion : " + eerVersion);
    }
}
