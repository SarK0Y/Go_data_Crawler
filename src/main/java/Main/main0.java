package Main;

import static basix_funx._basix_funx.prnt;
import _lsp._Server;
import _lsp._TxtDocSrv;
import basix_funx.loggy;
public class main0{
    private static _Server serv;
    public static void main (String[] args ) {
        serv = new _Server ();
        prnt ("tst" + 0);
         try {
            // Option 1: Stdio (most common for LSP)
           // serv.startStdio();
            
            // Option 2: TCP Socket (alternative)
             serv.startSocket(5007);
            
        } catch (Exception e) {
          //  e.printStackTrace();
            loggy.w.info(e.getMessage());
        }
    }
}