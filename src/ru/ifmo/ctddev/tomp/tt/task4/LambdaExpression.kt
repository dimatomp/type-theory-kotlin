import java.util.*

public abstract class Lambda() {
    private var parent: Lambda = this

    protected fun get(): Lambda {
        if (parent != this)
            parent = parent.get()
        return parent
    }

    protected fun unite(o: Lambda) {
        val left = get()
        val right = o.get()
        left.parent = right
    }

    protected fun stepSave(): Lambda {
        val result = step()
        if (parent == this) {
            unite(result)
            get()
        }
        return result
    }

    public fun normalize(): Lambda {
        while (get() != stepFarther());
        return get()
    }

    protected abstract fun step(): Lambda

    protected fun stepFarther(): Lambda {
        val res = get()
        val result = if (res == this) res.step() else res.stepFarther()
        res.unite(result)
        return result
    }

    public fun freeVars(): Set<String> {
        val answer: MutableSet<String> = HashSet()
        val used: MutableSet<String> = HashSet()
        fun recurse(cur: Lambda) {
            when (cur) {
                is Var -> if (cur.name !in used) answer.add(cur.name)
                is Lam -> {
                    val remove = cur.par !in used
                    used.add(cur.par)
                    recurse(cur.expr)
                    if (remove) used.remove(cur.par)
                }
                is App -> {
                    recurse(cur.fst)
                    recurse(cur.snd)
                }
            }
        }
        recurse(this)
        return answer
    }

    protected fun substitute(src: String, dest: Lambda): Lambda {
        val remapped: MutableMap<String, String> = HashMap()
        val used = dest.freeVars()
        fun recurse(cur: Lambda): Lambda {
            when (cur) {
                is Var -> when (cur.name) {
                    in remapped -> return Var(remapped.get(cur.name).orEmpty())
                    src -> return dest
                    else -> return cur
                }
                is Lam -> {
                    if (cur.par == src)
                        return cur
                    var newName = cur.par
                    while (newName in used || newName in remapped) newName += '\''
                    val prev = remapped.get(cur.par)
                    if (newName != cur.par)
                        remapped.put(cur.par, newName)
                    val expr = recurse(cur.expr)
                    if (newName != cur.par) {
                        if (prev == null)
                            remapped.remove(cur.par)
                        else
                            remapped.put(cur.par, prev)
                    }
                    return cur.modify(newName, expr)
                }
                is App -> return cur.modify(recurse(cur.fst), recurse(cur.snd))
            }
            throw IllegalArgumentException()
        }
        return recurse(this)
    }
}

public data class Var(val name: String): Lambda() {
    override fun step(): Lambda = this
    override fun toString(): String = name
}

public data class Lam(val par: String, val expr: Lambda): Lambda() {
    override fun step(): Lambda = modify(par, expr.stepFarther())

    fun modify(p: String, e: Lambda): Lam = if (p == par && e == expr) this else Lam(p, e)
    override fun toString(): String = "\\$par.$expr"
}

public data class App(val fst: Lambda, val snd: Lambda): Lambda() {
    override fun step(): Lambda = if (fst is Lam) fst.expr.substitute(fst.par, snd) else {
        val fStep = fst.stepSave()
        if (fStep is Lam) App(fStep, snd).stepFarther() else modify(fStep, snd.stepFarther())
    }

    fun modify(f: Lambda, s: Lambda): App = if (f == fst && s == snd) this else App(f, s)

    override fun toString(): String {
        val fSubst = if (fst is Lam) "($fst)" else fst.toString()
        val sSubst = if (snd !is Var) "($snd)" else snd.toString()
        return "$fSubst $sSubst"
    }
}
