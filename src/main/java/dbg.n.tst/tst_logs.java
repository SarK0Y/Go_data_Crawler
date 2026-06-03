package dbg.n.tst;
import basix_funx.loggy;
public class tst_logs {
   public static void main(String[] args) {
        loggy.w.info("Application started");
        loggy.w.warn("This is a warning");
        loggy.w.error("Something went wrong");
    }
}
// sh> java -DLOG_PATH=/tmp/loggy -cp  ./target/go_data_crawler.jar dbg.n.tst.tst_logs