package errors

final case class InvalidRecaptchaException(private val message: String = "Invalid recaptcha") extends Throwable(message)
final case class paymentNotCoveredException(private val message: String = "Payment not Covered") extends Throwable(message)
final case class failedTxException(private val message: String = "Tx sending failed") extends Throwable(message)
final case class explorerException(private val message: String = "Explorer error") extends Throwable(message)
final case class connectionException(private val message: String = "Network Error") extends Throwable(message)
final case class parseException(private val message: String = "Parsing failed") extends Throwable(message)
final case class finishedLendException(private val message: String = "raffle finished") extends Throwable(message)
final case class skipException(private val message: String = "skip") extends Throwable(message)
final case class proveException(private val message: String = "Tx proving failed") extends Throwable(message)
