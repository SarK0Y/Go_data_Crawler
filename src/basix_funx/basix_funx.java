package basix_funx;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
public class basix_funx {
    public static void prnt(String arg) {
        System.out.print(arg);
    }

    public static Path[] getPathsFromDir(String dir, String FilterByExt) {
        Path _dir = Paths.get(URI.create(dir));
        Path[] _Files = new Path[0];
        try (Stream<Path> s = Files.list(_dir)) {
            _Files = s.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(FilterByExt))
                    .toArray(Path[]::new);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return _Files;
    }
    public static Path[] getPathsFromDir(String dir) {
        Path _dir = Paths.get(URI.create(dir));
        Path[] _Files = new Path[0];
        try (Stream<Path> s = Files.list(_dir)) {
            _Files = s.filter(Files::isRegularFile)
                    .toArray(Path[]::new);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return _Files;
    }
}