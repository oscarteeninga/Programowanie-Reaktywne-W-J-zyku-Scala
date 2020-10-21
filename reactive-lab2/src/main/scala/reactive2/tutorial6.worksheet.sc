///////////////////////////////////////
// Case classes and Pattern matching //
///////////////////////////////////////

// selaed means that Expr class cannot be implement outside of this scope
sealed trait Expr
case class Number(n: Int) extends Expr
case class Sum(e1: Expr, e2: Expr) extends Expr

// Case classes are special classes:
// 1. The compiler automatically generates a companion object with method "apply" and "unapply"
//      object Number {
//          def apply(n: Int) = new Number(n)
//          def unapply(n: Number): Option[Int] = Some(n.n)
//      }
// 2. Parameters of the constructor are automatically members of the class
// 3. Immutable by default
// 4. Automatically defined getters
// 5. Default implementations of toString(), equals()
// 6. Automatically serializable
// 7. Can be used in pattern matching (see: unapply method)

// Case classes are useful for defining data structures

// Pattern matching is a kind of "generalized switch"
// We can match even objects: matching "decomposes" an object:
// - what class was used to construct the object?
// - what values were passed to the constructor?
def eval(e: Expr): Int = e match {
  case Number(n) => n
  case Sum(e1, e2) => eval(e1) + eval(e2)
}                                               //> eval: (e: reactive2.tutorial5.Expr)Int

val e = Sum(Sum(Number(2), Number(3)), Number(4)) //> e  : reactive2.tutorial5.Sum = Sum(Sum(Number(2),Number(3)),Number(4))
eval(e) //> res0: Int = 9

// Pattern matching can be used to create partial function
def eval2(e: Expr): Int = {
  val evalPF: PartialFunction[Expr, Int] = {
    case Number(n)   => n
    case Sum(e1, e2) => eval2(e1) + eval2(e2)
  }
  evalPF(e)
}

eval2(e)

// Other examples of patterns:
//   case 1                             -> matches constant "1"
//   case Sum(Number(1), Number(_))     -> matches object with specific structure ("_" is a wildcard)
//   case n: Int                        -> matches any Int value
//   case _                             -> matches anything

// Assignment TODO -- check the following:
// - What will happen when we add a new Expr type (e.g. 'case class Mul(e1: Expr, e2: Expr)') ? Will the current code compile?

