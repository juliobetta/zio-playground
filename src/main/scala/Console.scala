// Scala 3 syntax version of https://zio.dev/docs/overview/overview_background


enum Console[+A]:
  case Return(value: () => A)
  case PrintLine(line: String, rest: Console[A])
  case ReadLine(rest: String => Console[A])

object Console {
  def succeded[A](a: => A): Console[A] = Return(() => a)

  def printLine(line: String): Console[Unit] = PrintLine(line, succeded(()))

  val readLine: Console[String] = ReadLine(line => succeded(line))
}

extension [A](console: Console[A])
  def flatMap[B](f: A => Console[B]): Console[B] =
    console match {
      case Console.Return(value) => f(value())
      case Console.PrintLine(line, next) => Console.PrintLine(line, next.flatMap(f))
      case Console.ReadLine(next) => Console.ReadLine(line => next(line).flatMap(f))
    }

  def map[B](f: A => B): Console[B] = flatMap(a => Console.succeded(f(a)))

def main(args: List[String]) =
  val app: Console[String] = for {
    _ <- Console.printLine("ok")
    name <- Console.readLine
    _ <- Console.printLine(s"Hello, $name")
  } yield name
