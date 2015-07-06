public abstract class Lambda() {
    private var par: Lambda = this

    private fun get(): Lambda {
        if (par != this)
            par = par.get()
        return par
    }

    private fun unite(o: Lambda) {
        val left = get()
        val right = o.get()
        left.par = right
    }

    protected abstract fun step(): Lambda

    protected fun stepFarther(): Lambda {
        val res = get()
        val result = if (res == this) res.step() else res.stepFarther()
        res.unite(result)
        return result
    }
}
