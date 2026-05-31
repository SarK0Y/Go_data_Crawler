package basix_funx;
public class tuple<__0,__1> {
    public __0 _0;
    public __1 _1;
    public tuple(__0 _0, __1 _1) {
        this._0 = _0;
        this._1 = _1;
    }
    public tuple<__0,__1> clone() {
        return new tuple<>(_0, _1);
    }
}