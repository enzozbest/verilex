object slu_refimpl {

trait equal[A] {
  val `slu_refimpl.equal` : (A, A) => Boolean
}
def equal[A](a : A, b : A)(implicit A: equal[A]) : Boolean =
  A.`slu_refimpl.equal`(a, b)
object equal {
}

abstract sealed class nat
final case class zero_nat() extends nat
final case class Suc(a : nat) extends nat

abstract sealed class set[A]
final case class seta[A](a : List[A]) extends set[A]
final case class coset[A](a : List[A]) extends set[A]

abstract sealed class char
final case class Char(a : Boolean, b : Boolean, c : Boolean, d : Boolean,
                       e : Boolean, f : Boolean, g : Boolean, h : Boolean)
  extends char

abstract sealed class vala[A]
final case class Void[A]() extends vala[A]
final case class Atm[A](a : A) extends vala[A]
final case class Seq[A](a : vala[A], b : vala[A]) extends vala[A]
final case class Right[A](a : vala[A]) extends vala[A]
final case class Left[A](a : vala[A]) extends vala[A]
final case class Stars[A](a : List[vala[A]]) extends vala[A]
final case class Recv[A](a : List[char], b : vala[A]) extends vala[A]

abstract sealed class rexp[A]
final case class Zero[A]() extends rexp[A]
final case class One[A]() extends rexp[A]
final case class Atom[A](a : A) extends rexp[A]
final case class Plus[A](a : rexp[A], b : rexp[A]) extends rexp[A]
final case class Times[A](a : rexp[A], b : rexp[A]) extends rexp[A]
final case class Star[A](a : rexp[A]) extends rexp[A]
final case class NTimes[A](a : rexp[A], b : nat) extends rexp[A]
final case class Upto[A](a : rexp[A], b : nat) extends rexp[A]
final case class From[A](a : rexp[A], b : nat) extends rexp[A]
final case class Rec[A](a : List[char], b : rexp[A]) extends rexp[A]
final case class Charset[A](a : set[A]) extends rexp[A]

def eq[A : equal](a : A, b : A) : Boolean = equal[A](a, b)

def membera[A : equal](x0 : List[A], y : A) : Boolean = (x0, y) match {
  case (Nil, y) => false
  case (x :: xs, y) => eq[A](x, y) || membera[A](xs, y)
}

def member[A : equal](x : A, xa1 : set[A]) : Boolean = (x, xa1) match {
  case (x, coset(xs)) => ! (membera[A](xs, x))
  case (x, seta(xs)) => membera[A](xs, x)
}

def equal_nat(x0 : nat, x1 : nat) : Boolean = (x0, x1) match {
  case (zero_nat(), Suc(x2)) => false
  case (Suc(x2), zero_nat()) => false
  case (Suc(x2), Suc(y2)) => equal_nat(x2, y2)
  case (zero_nat(), zero_nat()) => true
}

def nullable[A](x0 : rexp[A]) : Boolean = x0 match {
  case Zero() => false
  case One() => true
  case Atom(c) => false
  case Plus(r1, r2) => nullable[A](r1) || nullable[A](r2)
  case Times(r1, r2) => nullable[A](r1) && nullable[A](r2)
  case Star(r) => true
  case NTimes(r, n) =>
    (equal_nat(n, zero_nat()) match { case true => true
      case false => nullable[A](r) })
  case Upto(r, n) => true
  case From(r, n) =>
    (equal_nat(n, zero_nat()) match { case true => true
      case false => nullable[A](r) })
  case Rec(l, r) => nullable[A](r)
  case Charset(cs) => false
}

def minus_nat(m : nat, n : nat) : nat = (m, n) match {
  case (Suc(m), Suc(n)) => minus_nat(m, n)
  case (zero_nat(), n) => zero_nat()
  case (m, zero_nat()) => m
}

def one_nat : nat = Suc(zero_nat())

def deriv[A : equal](c : A, x1 : rexp[A]) : rexp[A] = (c, x1) match {
  case (c, Zero()) => Zero[A]()
  case (c, One()) => Zero[A]()
  case (ca, Atom(c)) =>
    (eq[A](ca, c) match { case true => One[A]() case false => Zero[A]() })
  case (c, Plus(r1, r2)) => Plus[A](deriv[A](c, r1), deriv[A](c, r2))
  case (c, Times(r1, r2)) =>
    (nullable[A](r1) match {
      case true => Plus[A](Times[A](deriv[A](c, r1), r2), deriv[A](c, r2))
      case false => Times[A](deriv[A](c, r1), r2) })
  case (c, Star(r)) => Times[A](deriv[A](c, r), Star[A](r))
  case (c, NTimes(r, n)) =>
    (equal_nat(n, zero_nat()) match { case true => Zero[A]()
      case false => Times[A](deriv[A](c, r),
                              NTimes[A](r, minus_nat(n, one_nat)))
      })
  case (c, Upto(r, n)) =>
    (equal_nat(n, zero_nat()) match { case true => Zero[A]()
      case false => Times[A](deriv[A](c, r), Upto[A](r, minus_nat(n, one_nat)))
      })
  case (c, From(r, n)) =>
    (equal_nat(n, zero_nat()) match {
      case true => Times[A](deriv[A](c, r), Star[A](r))
      case false => Times[A](deriv[A](c, r), From[A](r, minus_nat(n, one_nat)))
      })
  case (c, Rec(l, r)) => deriv[A](c, r)
  case (c, Charset(cs)) =>
    (member[A](c, cs) match { case true => One[A]() case false => Zero[A]() })
}

def replicate[A](xa0 : nat, x : A) : List[A] = (xa0, x) match {
  case (zero_nat(), x) => Nil
  case (Suc(n), x) => x :: replicate[A](n, x)
}

def mkeps[A](x0 : rexp[A]) : vala[A] = x0 match {
  case One() => Void[A]()
  case Times(r1, r2) => Seq[A](mkeps[A](r1), mkeps[A](r2))
  case Plus(r1, r2) =>
    (nullable[A](r1) match { case true => Left[A](mkeps[A](r1))
      case false => Right[A](mkeps[A](r2)) })
  case Star(r) => Stars[A](Nil)
  case Upto(r, n) => Stars[A](Nil)
  case NTimes(r, n) => Stars[A](replicate[vala[A]](n, mkeps[A](r)))
  case From(r, n) => Stars[A](replicate[vala[A]](n, mkeps[A](r)))
  case Rec(l, r) => Recv[A](l, mkeps[A](r))
}

def injval[A](x0 : rexp[A], c : A, v : vala[A]) : vala[A] = (x0, c, v) match {
  case (Atom(d), c, Void()) => Atm[A](c)
  case (Plus(r1, r2), c, Left(v1)) => Left[A](injval[A](r1, c, v1))
  case (Plus(r1, r2), c, Right(v2)) => Right[A](injval[A](r2, c, v2))
  case (Times(r1, r2), c, Seq(v1, v2)) => Seq[A](injval[A](r1, c, v1), v2)
  case (Times(r1, r2), c, Left(Seq(v1, v2))) => Seq[A](injval[A](r1, c, v1), v2)
  case (Times(r1, r2), c, Right(v2)) =>
    Seq[A](mkeps[A](r1), injval[A](r2, c, v2))
  case (Star(r), c, Seq(v, Stars(vs))) => Stars[A](injval[A](r, c, v) :: vs)
  case (NTimes(r, n), c, Seq(v, Stars(vs))) =>
    Stars[A](injval[A](r, c, v) :: vs)
  case (Upto(r, n), c, Seq(v, Stars(vs))) => Stars[A](injval[A](r, c, v) :: vs)
  case (From(r, n), c, Seq(v, Stars(vs))) => Stars[A](injval[A](r, c, v) :: vs)
  case (Rec(l, r), c, v) => Recv[A](l, injval[A](r, c, v))
  case (Charset(cs), c, Void()) => Atm[A](c)
}

def lexer[A : equal](r : rexp[A], x1 : List[A]) : Option[vala[A]] = (r, x1)
  match {
  case (r, Nil) =>
    (nullable[A](r) match { case true => Some[vala[A]](mkeps[A](r))
      case false => None })
  case (r, c :: s) => (lexer[A](deriv[A](c, r), s) match {
                         case None => None
                         case Some(v) => Some[vala[A]](injval[A](r, c, v))
                       })
}

} /* object slu_refimpl */
