/**
 * 2020 FALL CMPT435 Project
 * Name: Yue Weng
 * NSID: yuw857
 * Student number: 1121 9127
 */


/**
 * Set all exceptions here
 */



trait ServerException extends Exception

case class TestException(message:String) extends ServerException
case class RestartException(message:String) extends ServerException
case class StopActorException(message:String) extends ServerException
