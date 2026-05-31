package cmdLine;
import cmdLine.TypeOfTask;
import cmdLine.ModeOfTask;
import io.vavr.control.Either;
import java.util.List;
import java.util.ArrayList;
import cmdLine.cmd_line;
import basix_funx.tuple;
public class BaseCmdLine implements cmd_line <BaseCmdLine> {
    private ModeOfTask taskMode = ModeOfTask.none;
    private TypeOfTask taskType = TypeOfTask.singleKey;
    private String [] args;
    static Either <BaseCmdLine, Integer> create (ModeOfTask setMode, TypeOfTask setType, String [] args) {
        var base = new BaseCmdLine (setMode, setType, args );
        return Either.left(base);
    }
    public BaseCmdLine (ModeOfTask setMode, TypeOfTask setType, String [] args) {
        this.args = (args == null) ? new String[0] : args.clone();
        this.taskMode = setMode;
        this.taskType = setType;
    }
    @Override
    public void exec () {
        switch (taskMode) {
            case do_1st:
                this.do_1st ();
                break;
            case do_lst:
                this.do_last ();
                break;
            case do_all:
                this.do_all();
        }
    }
    @Override
    public void do_all (){
    }
    @Override public void do_1st (){}
    @Override public void do_last (){}
    @Override public List <tuple <String, String> > AllOneKeyOneVal (String... keys) {
        var got_key = false;
        tuple <String, String> _tuple = new tuple <>("","");
        List <tuple <String, String> > ret = new ArrayList <> ();
        for (var key: keys) {
            for (var arg: args) {
                if (arg.equals (key) && !got_key) {
                    _tuple._0 = key;
                    got_key = true;
                }
                if (got_key) {
                    _tuple._1 = arg;
                    ret.add(_tuple.clone());
                    got_key = false;
                }
            }
        }
        return ret;
    }
}