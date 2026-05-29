package cmdLine;
import cmdLine.TypeOfTask;
import cmdLine.ModeOfTask;
import io.vavr.control.Either;
import cmdLine.cmd_line;
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
}