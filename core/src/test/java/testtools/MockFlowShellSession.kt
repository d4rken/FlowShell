//package testtools
//
//import eu.darken.rxshell.shell.RxShell
//import io.reactivex.rxjava3.core.Completable
//import io.reactivex.rxjava3.core.Single
//import io.reactivex.rxjava3.processors.PublishProcessor
//import io.reactivex.rxjava3.processors.ReplayProcessor
//import timber.log.Timber
//import org.mockito.ArgumentMatchers.any
//import org.mockito.ArgumentMatchers.anyBoolean
//import org.mockito.Mockito.doAnswer
//import org.mockito.Mockito.mock
//import org.mockito.Mockito.`when`
//import java.util.concurrent.LinkedBlockingQueue
//
//class MockFlowShellSession {
//    var outputPub: PublishProcessor<String>
//    var errorPub: PublishProcessor<String>
//    var waitForPub: ReplayProcessor<Int> = ReplayProcessor.create()
//    var queue = LinkedBlockingQueue<String>()
//    val session: RxShell.Session
//
//    init {
//        val thread = Thread(label@ Runnable {
//            while (true) {
//                var line: String
//                line = try {
//                    queue.take()
//                } catch (e: InterruptedException) {
//                    Timber.e(e)
//                    return@label
//                }
//                if (line.endsWith(" $?")) {
//                    // By default we assume all commands exit OK
//                    val split = line.split(" ").toTypedArray()
//                    outputPub.onNext(split[1] + " " + 0)
//                } else if (line.endsWith(" >&2")) {
//                    val split = line.split(" ").toTypedArray()
//                    errorPub.onNext(split[1])
//                } else if (line.startsWith("sleep")) {
//                    val split = line.split(" ").toTypedArray()
//                    val delay = split[1].toLong()
//                    Timber.v("Sleeping for %d", delay)
//                    TestHelper.sleep(delay)
//                } else if (line.startsWith("echo")) {
//                    val split = line.split(" ").toTypedArray()
//                    outputPub.onNext(split[1])
//                } else if (line.startsWith("error")) {
//                    val split = line.split(" ").toTypedArray()
//                    errorPub.onNext(split[1])
//                } else if (line.startsWith("exit")) {
//                    break
//                }
//            }
//            outputPub.onComplete()
//            errorPub.onComplete()
//            waitForPub.onNext(0)
//            waitForPub.onComplete()
//        })
//        thread.start()
//        session = mock(RxShell.Session::class.java)
//        doAnswer { invocation ->
//            val line: String = invocation.getArgument(0)
//            val flush: Boolean = invocation.getArgument(1)
//            Timber.d("writeLine(%s, %b)", line, flush)
//            queue.add(line)
//            null
//        }.`when`(session).writeLine(any(), anyBoolean())
//        outputPub = PublishProcessor.create()
//        `when`(session.outputLines()).thenReturn(outputPub)
//        errorPub = PublishProcessor.create()
//        `when`(session.errorLines()).thenReturn(errorPub)
//        `when`(session.cancel()).then { invocation ->
//            Completable.create { e ->
//                Timber.i("cancel()")
//                thread.interrupt()
//                outputPub.onComplete()
//                errorPub.onComplete()
//                waitForPub.onNext(1)
//                waitForPub.onComplete()
//                e.onComplete()
//            }
//        }
//        val close: Single<Int> = Completable.create { e ->
//            queue.add("exit" + LineReader.getLineSeparator())
//            e.onComplete()
//        }.andThen(waitForPub.lastOrError()).cache()
//        `when`(session.close()).thenReturn(close)
//        waitForPub.doOnEach { integerNotification -> Timber.i("waitFor %s", integerNotification) }.subscribe()
//        `when`(session.waitFor()).thenReturn(waitForPub.lastOrError())
//        `when`(session.isAlive()).thenReturn(Single.create { e -> e.onSuccess(thread.isAlive) })
//    }
//
//    fun getSession(): RxShell.Session {
//        return session
//    }
//
//    fun getErrorPub(): PublishProcessor<String> {
//        return errorPub
//    }
//
//    fun getOutputPub(): PublishProcessor<String> {
//        return outputPub
//    }
//}