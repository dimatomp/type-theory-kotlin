import App
import Lam
import Lambda
import Var
import java.util.HashMap
import java.util.HashSet

public abstract class Lambda() {
    private var parent: Lambda = this

    protected fun get(): Lambda {
        if (parent != this)
            parent = parent.get()
        return parent
    }

    protected fun unite(o: Lambda) {
        get().parent = o.get()
    }

    public fun normalize(): Lambda {
        while (get() != stepFarther());
        return get()
    }

    protected fun stepFarther(): Lambda {
        val p = get()
        val result = if (p == this) p.step() else p.stepFarther()
        unite(result)
        return get()
    }

    protected abstract fun step(): Lambda

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
        fun recurse(cur: Lambda, subst: Boolean): Lambda {
            when (cur) {
                is Var -> return when (cur.name) {
                    in remapped -> Var(remapped.get(cur.name) as String)
                    src -> if (subst) dest else cur
                    else -> cur
                }
                is Lam -> {
                    val substR = subst && cur.par != src
                    val fv = cur.freeVars()
                    val newName: String = StringBuilder {
                        append(cur.par)
                        var res = toString()
                        while (res in used || res in fv) {
                            append('\'')
                            res = toString()
                        }
                    }.toString()
                    val prev = remapped.get(cur.par)
                    if (newName != cur.par)
                        remapped.put(cur.par, newName)
                    val expr = recurse(cur.expr, substR)
                    if (newName != cur.par) {
                        if (prev == null)
                            remapped.remove(cur.par)
                        else
                            remapped.put(cur.par, prev)
                    }
                    return cur.modify(newName, expr)
                }
                is App -> return cur.modify(recurse(cur.fst, subst), recurse(cur.snd, subst))
            }
            throw IllegalArgumentException()
        }
        return recurse(this, true)
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
        val fStep = fst.stepFarther()
        if (fst == fStep) modify(fst, snd.normalize()) else modify(fStep, snd)
    }

    fun modify(f: Lambda, s: Lambda): App = if (f == fst && s == snd) this else App(f, s)

    override fun toString(): String {
        val fSubst = if (fst is Lam) "($fst)" else fst.toString()
        val sSubst = if (snd !is Var) "($snd)" else snd.toString()
        return "$fSubst $sSubst"
    }
}
