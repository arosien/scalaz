package scalaz

sealed trait \/[+A, +B] {
  sealed trait Switching_\/[X] {
    def r: X
    def <<?:(left: => X): X =
      \/.this match {
        case -\/(_) => left
        case \/-(_) => r
      }
  }

  def :?>>[X](right: => X): Switching_\/[X] =
    new Switching_\/[X] {
      def r = right
    }

  def isLeft: Boolean =
    this match {
      case -\/(_) => true
      case \/-(_) => false
    }

  def isRight: Boolean =
    this match {
      case -\/(_) => false
      case \/-(_) => true
    }

  def fold[X](l: A => X, r: B => X): X =
    this match {
      case -\/(a) => l(a)
      case \/-(b) => r(b)
    }

  def swap: (B \/ A) =
    this match {
      case -\/(a) => \/-(a)
      case \/-(b) => -\/(b)
    }

  def unary_~ : (B \/ A) =
    swap

  def swapped[AA >: A, BB >: B](k: (B \/ A) => (BB \/ AA)): (AA \/ BB) =
    k(swap).swap

  def ~[AA >: A, BB >: B](k: (B \/ A) => (BB \/ AA)): (AA \/ BB) =
    swapped(k)

  def bimap[C, D](f: A => C, g: B => D): (C \/ D) =
    this match {
      case -\/(a) => -\/(f(a))
      case \/-(b) => \/-(g(b))
    }

  def bitraverse[F[+_]: Functor, C, D](f: A => F[C], g: B => F[D]): F[C \/ D] =
    this match {
      case -\/(a) => Functor[F].map(f(a))(-\/(_))
      case \/-(b) => Functor[F].map(g(b))(\/-(_))
    }

  def map[D](g: B => D): (A \/ D) =
    bimap(identity, g)

  def traverse[F[+_]: Applicative, D](g: B => F[D]): F[A \/ D] =
    this match {
      case -\/(a) => Applicative[F].point(-\/(a))
      case \/-(b) => Functor[F].map(g(b))(\/-(_))
    }

  def foreach(g: B => Unit): Unit =
    bimap(_ => (), g)

  def flatMap[AA >: A, D](g: B => (AA \/ D)): (AA \/ D) =
    this match {
      case -\/(a) => -\/(a)
      case \/-(b) => g(b)
    }

  def foldRight[Z](z: => Z)(f: (B, => Z) => Z): Z =
    this match {
      case -\/(_) => z
      case \/-(a) => f(a, z)
    }

  def filter[BB >: B](p: BB => Boolean)(implicit M: Monoid[BB]): (A \/ BB) =
    this match {
      case -\/(a) => -\/(a)
      case \/-(b) => \/-(if(p(b)) b else M.zero)
    }

  def exists[BB >: B](p: BB => Boolean): Boolean =
    this match {
      case -\/(_) => false
      case \/-(b) => p(b)
    }

  def forall[BB >: B](p: BB => Boolean): Boolean =
    this match {
      case -\/(_) => true
      case \/-(b) => p(b)
    }

  def toList: List[B] =
    this match {
      case -\/(_) => Nil
      case \/-(b) => List(b)
    }

  def toStream: Stream[B] =
    this match {
      case -\/(_) => Stream()
      case \/-(b) => Stream(b)
    }

  def toOption: Option[B] =
    this match {
      case -\/(_) => None
      case \/-(b) => Some(b)
    }

  def toEither: Either[A, B] =
    this match {
      case -\/(a) => Left(a)
      case \/-(b) => Right(b)
    }

  def getOrElse[BB >: B](x: => BB): BB =
    toOption getOrElse x

  def ?[BB >: B](x: => BB): BB =
    getOrElse(x)

  def valueOr[BB >: B](x: A => BB): BB =
    this match {
      case -\/(a) => x(a)
      case \/-(b) => b
    }

  def orElse[AA >: A, BB >: B](x: => AA \/ BB): AA \/ BB =
    this match {
      case -\/(_) => x
      case \/-(_) => this
    }

  def |||[AA >: A, BB >: B](x: => AA \/ BB): AA \/ BB =
    orElse(x)

  def ++[AA >: A, BB >: B](x: => AA \/ BB)(implicit M: Semigroup[BB]): AA \/ BB =
    this match {
      case -\/(_) => this
      case \/-(b1) => x match {
        case -\/(a2) => -\/(a2)
        case \/-(b2) => \/-(M.append(b1, b2))
      }
    }

  def ===[AA >: A, BB >: B](x: AA \/ BB)(implicit EA: Equal[AA], EB: Equal[BB]): Boolean =
    this match {
      case -\/(a1) => x match {
        case -\/(a2) => Equal[AA].equal(a1, a2)
        case \/-(_) => false
      }
      case \/-(b1) => x match {
        case \/-(b2) => Equal[BB].equal(b1, b2)
        case -\/(_) => false
      }
    }

  def compare[AA >: A, BB >: B](x: AA \/ BB)(implicit EA: Order[AA], EB: Order[BB]): Ordering =
    this match {
      case -\/(a1) => x match {
        case -\/(a2) => Order[AA].apply(a1, a2)
        case \/-(_) => Ordering.LT
      }
      case \/-(b1) => x match {
        case \/-(b2) => Order[BB].apply(b1, b2)
        case -\/(_) => Ordering.GT
      }
    }

  def show[AA >: A, BB >: B](implicit SA: Show[AA], SB: Show[BB]): List[Char] = 
    this match {
      case -\/(a) => List('-', '\\', '/', '(') ::: SA.show(a) ::: List(')')
      case \/-(b) => List('\\', '/', '-', '(') ::: SB.show(b) ::: List(')')
    }

}
case class -\/[+A](a: A) extends (A \/ Nothing)
case class \/-[+B](b: B) extends (Nothing \/ B)

object \/ extends Instances_\/

trait Instances_\/ extends Instances0_\/ {
  type GlorifiedTuple[+A, +B] =
  A \/ B
}

trait Instances0_\/ extends Instances1_\/ {
  implicit def Order_\/[A: Order, B: Order]: Order[A \/ B] =
    new Order[A \/ B] {
      def order(a1: A \/ B, a2: A \/ B) =
        a1 compare a2
    }

  implicit def Monoid_\/[A: Monoid, B: Semigroup]: Monoid[A \/ B] =
    new Monoid[A \/ B] {
      def append(a1: A \/ B, a2: => A \/ B) =
        a1 ++ a2
      def zero =
        -\/(Monoid[A].zero)
    }
}

trait Instances1_\/ extends Instances2_\/ {
  implicit def Equal_\/[A: Equal, B: Equal]: Equal[A \/ B] =
    new Equal[A \/ B] {
      def equal(a1: A \/ B, a2: A \/ B) =
        a1 === a2
    }

  implicit def Show_\/[A: Show, B: Show]: Show[A \/ B] =
    Show.show(_.show)

  implicit def Semigroup_\/[A, B: Semigroup]: Semigroup[A \/ B] =
    new Semigroup[A \/ B] {
      def append(a1: A \/ B, a2: => A \/ B) =
        a1 ++ a2
    }
}

trait Instances2_\/ extends Instances3_\/ {
  implicit def Instances1_\/[L]: Traverse[({type l[a] = L \/ a})#l] with Monad[({type l[a] = L \/ a})#l] with Cozip[({type l[a] = L \/ a})#l] = new Traverse[({type l[a] = L \/ a})#l] with Monad[({type l[a] = L \/ a})#l] with Cozip[({type l[a] = L \/ a})#l] {
    def bind[A, B](fa: L \/ A)(f: A => L \/ B) =
      fa flatMap f

    def point[A](a: => A) =
      \/-(a)

    def traverseImpl[G[+_] : Applicative, A, B](fa: L \/ A)(f: (A) => G[B]) =
      fa.traverse(f)

    override def foldRight[A, B](fa: L \/ A, z: => B)(f: (A, => B) => B) =
      fa.foldRight(z)(f)

    def cozip[A, B](x: L \/ (A \/ B)) =
      x match {
        case -\/(l) => -\/(-\/(l))
        case \/-(e) => e match {
          case -\/(a) => -\/(\/-(a))
          case \/-(b) => \/-(\/-(b))
        }
      }
  }

}

trait Instances3_\/ {
  implicit def Instances0_\/ : Bitraverse[\/] = new Bitraverse[\/] {
    override def bimap[A, B, C, D](fab: A \/ B)
                                  (f: A => C, g: B => D) = fab bimap (f, g)

    def bitraverseImpl[G[+_] : Applicative, A, B, C, D](fab: A \/ B)
                                                  (f: A => G[C], g: B => G[D]) =
      fab.bitraverse(f, g)
  }
}