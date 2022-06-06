package registers

class RegisterTypeException(msg: String) extends RuntimeException(msg)
object RegisterTypeException {
  def apply(msg: String): Nothing = {
    throw new RegisterTypeException(msg)
  }
}
