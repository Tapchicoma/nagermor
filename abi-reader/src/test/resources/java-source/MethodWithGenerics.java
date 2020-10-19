import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MethodWithGenerics {
    public List<Base> calculateSomething(
        List<Set<BaseInterface>> param1
    ) {
        return new ArrayList<Base>();
    }
}
