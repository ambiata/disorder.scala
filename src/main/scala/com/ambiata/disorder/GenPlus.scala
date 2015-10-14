package com.ambiata.disorder

import org.scalacheck._, Arbitrary._, Gen._
import scala.collection.JavaConverters._

import scalaz._, Scalaz._

object GenPlus {

  def chooseInt(from: Int, to: Int): Gen[Int] =
    Gen.sized(n => Gen.choose(from, math.max(from, math.min(n, to))))

  def chooseLong(from: Long, to: Long): Gen[Long] =
    Gen.sized(n => Gen.choose(from, math.max(from, math.min(n.toLong, to))))

  // The Gen version of this function results in too many discarded tests
  def nonEmptyListOf[A](gen: => Gen[A]): Gen[List[A]] =
    listOfSized(0, Int.MaxValue, gen)

  /**
   * A handy replacement for:
   *
   * {{{
   *   n <- Gen.choose(x, y)
   *   _ <- Gen.listOfN(n, ...)
   * }}}
   */
  def listOfSized[A](from: Int, to: Int, gen: => Gen[A]): Gen[List[A]] =
    chooseInt(from, to).flatMap(i => Gen.listOfN(i, gen))

  /** Convenient way of tagging generators with the index */
  def listOfSizedWithIndex[A](from: Int, to: Int, gen: Int => Gen[A]): Gen[List[A]] =
    chooseInt(from, to).flatMap(i => Gen.sequence((0 until i).toList.map(gen)).map(_.asScala.toList))

  /*
   Safely generate an arbitrary which matches a predicate. This function will recursively
   attempt to create the arbitrary until it matches the predicate without failing, unless
   the number of attempts is greater than 100.
   */
  def checkGen[A](gen: Gen[A], label: String, check: A => Boolean): Gen[A] =
    checkGenX(0, gen, label, check)

  def checkGenX[A](n: Int, gen: Gen[A], label: String, check: A => Boolean): Gen[A] = {
    if (n > 100) Gen.fail.label(s"Couldn't generate a $label")
    else for {
      a <- gen
      z <- Gen.size
      r <- if (check(a)) Gen.const(a) else Gen.resize(z + 1, checkGenX[A](n + 1, gen, label, check))
    } yield r
  }

  def checkGenTuple[A, B: Arbitrary, C: Arbitrary](make: (B, C) => A, label: String, check: A => Boolean): Gen[A] = for {
    b <- arbitrary[B]
    c <- arbitrary[C]
    a <- make(b, c)
    r <- checkGen(a, label, check)
  } yield r

  implicit def GenMonad: Monad[Gen] = new Monad[Gen] {

    def point[A](a: => A): Gen[A] = Gen.const(a)

    def bind[A, B](fa: Gen[A])(f: A => Gen[B]): Gen[B] = fa.flatMap(f)

  }
}
