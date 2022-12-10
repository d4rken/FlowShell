//package rxshell.shell;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.junit.MockitoJUnitRunner;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import eu.darken.rxshell.process.RxProcess;
//import io.reactivex.rxjava3.core.Completable;
//import io.reactivex.rxjava3.core.Single;
//import io.reactivex.rxjava3.core.SingleEmitter;
//import io.reactivex.rxjava3.observers.TestObserver;
//import io.reactivex.rxjava3.schedulers.Schedulers;
//import io.reactivex.rxjava3.subjects.ReplaySubject;
//import io.reactivex.rxjava3.subscribers.TestSubscriber;
//import testtools.BaseTest;
//import testtools.MockInputStream;
//import testtools.MockOutputStream;
//import timber.log.Timber;
//
//import static org.awaitility.Awaitility.await;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.not;
//import static org.hamcrest.core.Is.is;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@RunWith(MockitoJUnitRunner.class)
//public class RxShellTest extends BaseTest {
//    @Mock RxProcess rxProcess;
//    @Mock RxProcess.Session rxProcessSession;
//    MockOutputStream cmdStream;
//    MockInputStream outputStream;
//    MockInputStream errorStream;
//    ReplaySubject<RxProcess.Session> sessionPub;
//    SingleEmitter<Integer> waitForEmitter;
//
//    @Before
//    public void setup() throws Exception {
//        super.setup();
//        sessionPub = ReplaySubject.create();
//        sessionPub.onNext(rxProcessSession);
//        when(rxProcess.open()).thenAnswer(invocation -> {
//            when(rxProcessSession.waitFor()).thenReturn(Single.create(e -> waitForEmitter = e));
//            return sessionPub.firstOrError();
//        });
//
//        cmdStream = new MockOutputStream(new MockOutputStream.Listener() {
//            @Override
//            public void onNewLine(String line) {
//                if (line.equals("exit" + LineReader.getLineSeparator())) {
//                    try {
//                        cmdStream.close();
//                    } catch (IOException e) {
//                        Timber.e(e);
//                    } finally {
//                        waitForEmitter.onSuccess(0);
//                    }
//                }
//            }
//
//            @Override
//            public void onClose() {
//
//            }
//        });
//        outputStream = new MockInputStream();
//        errorStream = new MockInputStream();
//
//        when(rxProcessSession.input()).thenReturn(cmdStream);
//        when(rxProcessSession.output()).thenReturn(outputStream);
//        when(rxProcessSession.error()).thenReturn(errorStream);
//        when(rxProcessSession.isAlive()).thenReturn(Single.create(e -> e.onSuccess(cmdStream.isOpen())));
//
//        when(rxProcessSession.destroy()).then(invocation -> Completable.create(e -> {
//            cmdStream.close();
//            waitForEmitter.onSuccess(1);
//            e.onComplete();
//        }));
//    }
//
//    @Test
//    public void testSession() throws IOException {
//        RxShell rxShell = new RxShell(rxProcess);
//
//        TestObserver<RxShell.Session> sessionObs = rxShell.open().test();
//        RxShell.Session session = sessionObs.awaitCount(1).assertNoErrors().values().get(0);
//        verify(rxProcess).open();
//
//        session.writeLine("test", true);
//
//        await().atMost(2, TimeUnit.SECONDS).until(() -> cmdStream.getData().toString().contains("test" + LineReader.getLineSeparator()));
//
//        TestSubscriber<String> outputObs = sessionObs.values().get(0).outputLines().test();
//        TestSubscriber<String> errorObs = sessionObs.values().get(0).errorLines().test();
//
//        verify(rxProcessSession).input();
//        verify(rxProcessSession).output();
//        verify(rxProcessSession).error();
//
//        outputStream.queue("outputtest" + LineReader.getLineSeparator());
//        errorStream.queue("errortest" + LineReader.getLineSeparator());
//
//        // Simulate process cleanly exiting
//        sessionPub.onComplete();
//
//        outputObs.awaitCount(1).assertNoErrors().assertValue("outputtest");
//        outputStream.close();
//
//
//        errorObs.awaitCount(1).assertNoErrors().assertValue("errortest");
//        errorStream.close();
//
//        sessionObs.awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertComplete();
//        outputObs.awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertComplete();
//        errorObs.awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertComplete();
//    }
//
//    @Test
//    public void testOpen_error() throws IOException {
//        doReturn(Single.error(new InterruptedException())).when(rxProcess).open();
//        RxShell rxShell = new RxShell(rxProcess);
//
//        TestObserver<RxShell.Session> sessionObs = rxShell.open().test();
//
//        sessionObs.awaitDone(1, TimeUnit.SECONDS).assertError(InterruptedException.class);
//        sessionObs.assertNoValues();
//    }
//
//    @Test
//    public void testIsAlive() {
//        RxShell rxShell = new RxShell(rxProcess);
//        RxShell.Session session = rxShell.open().test().awaitCount(1).assertNoErrors().values().get(0);
//        session.isAlive().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(true);
//
//        when(rxProcessSession.isAlive()).thenReturn(Single.just(false));
//        session.isAlive().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(false);
//
//        when(rxProcessSession.isAlive()).thenReturn(Single.just(true));
//        session.isAlive().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(true);
//
//        verify(rxProcessSession, times(3)).isAlive();
//    }
//
//    @Test
//    public void testIsAlive_indirect() throws InterruptedException {
//        RxShell rxShell = new RxShell(rxProcess);
//
//        when(rxProcessSession.isAlive()).thenReturn(Single.just(false));
//        rxShell.isAlive().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(false);
//
//        // No session means not alive!
//        when(rxProcessSession.isAlive()).thenReturn(Single.just(true));
//        rxShell.isAlive().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(false);
//        rxShell.open().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors();
//        rxShell.isAlive().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(true);
//
//        when(rxProcessSession.isAlive()).thenReturn(Single.just(false));
//        rxShell.isAlive().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(false);
//    }
//
//    @Test
//    public void testWaitFor() throws InterruptedException {
//        RxShell rxShell = new RxShell(rxProcess);
//        RxShell.Session session = rxShell.open().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().values().get(0);
//        assertThat(session.waitFor().test().await(1, TimeUnit.SECONDS), is(false));
//
//        waitForEmitter.onSuccess(55);
//        session.waitFor().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(55);
//    }
//
//    @Test
//    public void testCancel() {
//        RxShell rxShell = new RxShell(rxProcess);
//        RxShell.Session session = rxShell.open().test().awaitCount(1).assertNoErrors().values().get(0);
//        session.isAlive().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(true);
//
//        session.cancel().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertComplete();
//
//        session.isAlive().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(false);
//
//        // Idempotent
//        session.cancel().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertComplete();
//
//        verify(rxProcessSession).destroy();
//    }
//
//    @Test
//    public void testCancel_indirect() {
//        RxShell rxShell = new RxShell(rxProcess);
//        rxShell.cancel().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertComplete();
//
//        RxShell.Session session = rxShell.open().test().awaitCount(1).assertNoErrors().values().get(0);
//        session.isAlive().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(true);
//
//        rxShell.cancel().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertComplete();
//
//        // Idempotent
//        session.isAlive().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(false);
//    }
//
//    @Test
//    public void testClose() throws IOException {
//        RxShell rxShell = new RxShell(rxProcess);
//        RxShell.Session session = rxShell.open().test().awaitDone(10, TimeUnit.SECONDS).assertNoErrors().values().get(0);
//
//        session.close().test().awaitDone(10, TimeUnit.SECONDS).assertNoErrors().assertValue(0);
//
//        await().atMost(2, TimeUnit.SECONDS).until(() -> cmdStream.getData().toString().contains("exit" + LineReader.getLineSeparator()));
//        await().atMost(1, TimeUnit.SECONDS).until(() -> cmdStream.isOpen(), is(false));
//
//        // Idempotent
//        session.close().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(0);
//    }
//
//    @Test
//    public void testClose_exception() {
//        RxShell rxShell = new RxShell(rxProcess);
//        RxShell.Session session = rxShell.open().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().values().get(0);
//
//        cmdStream.setExceptionOnClose(new IOException());
//
//        session.close().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertNoErrors().assertValue(0);
//
//        await().atMost(2, TimeUnit.SECONDS).until(() -> cmdStream.getData().toString().contains("exit" + LineReader.getLineSeparator()));
//        await().atMost(1, TimeUnit.SECONDS).until(() -> cmdStream.isOpen(), is(false));
//
//        // Idempotent
//        session.close().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertNoErrors().assertValue(0);
//    }
//
//    @Test
//    public void testClose_indirect() {
//        RxShell rxShell = new RxShell(rxProcess);
//        rxShell.close().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(0);
//
//        RxShell.Session session = rxShell.open().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().values().get(0);
//        session.close().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(0);
//
//        await().atMost(2, TimeUnit.SECONDS).until(() -> cmdStream.getData().toString().contains("exit" + LineReader.getLineSeparator()));
//        await().atMost(1, TimeUnit.SECONDS).until(() -> cmdStream.isOpen(), is(false));
//
//        // Idempotent
//        session.close().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(0);
//    }
//
//    @Test
//    public void testClose_afterCancel() throws IOException {
//        RxShell rxShell = new RxShell(rxProcess);
//        RxShell.Session session = rxShell.open().test().awaitCount(1).assertNoErrors().values().get(0);
//        session.isAlive().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(true);
//
//        session.cancel().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertComplete();
//        rxShell.close().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(0);
//    }
//
//    @Test
//    public void testClose_raceconditions() {
//        RxShell rxShell = new RxShell(rxProcess);
//        when(rxProcessSession.waitFor()).thenReturn(Single.just(0).delay(100, TimeUnit.MILLISECONDS));
//        RxShell.Session session = rxShell.open().test().awaitCount(1).assertNoErrors().values().get(0);
//
//        int cnt = 1000;
//        List<TestObserver<Integer>> testObservers = new ArrayList<>();
//        for (int i = 0; i < cnt; i++)
//            testObservers.add(session.close().observeOn(Schedulers.newThread()).test());
//
//        assertThat(testObservers.size(), is(cnt));
//
//        for (TestObserver<Integer> t : testObservers)
//            t.awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(0);
//
//        assertThat(cmdStream.getData().toString(), is("exit" + LineReader.getLineSeparator()));
//    }
//
//    @Test
//    public void testProcessCompletion_linereaders_dont_terminate_early() throws IOException, InterruptedException {
//        RxShell rxShell = new RxShell(rxProcess);
//        RxShell.Session session = rxShell.open().test().awaitCount(1).assertComplete().values().get(0);
//        TestSubscriber<String> outputObs = session.outputLines().test();
//        TestSubscriber<String> errorObs = session.errorLines().test();
//
//        session.close().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(0);
//
//        await().atMost(1, TimeUnit.SECONDS).until(() -> cmdStream.isOpen(), is(false));
//
//        outputObs.assertNotComplete();
//        errorObs.assertNotComplete();
//
//        await().atMost(1, TimeUnit.SECONDS).until(() -> outputStream.isOpen(), is(true));
//        await().atMost(1, TimeUnit.SECONDS).until(() -> errorStream.isOpen(), is(true));
//
//        outputStream.close();
//        errorStream.close();
//
//        await().atMost(1, TimeUnit.SECONDS).until(() -> outputStream.isOpen(), is(false));
//        await().atMost(1, TimeUnit.SECONDS).until(() -> errorStream.isOpen(), is(false));
//
//        outputObs.assertComplete();
//        errorObs.assertComplete();
//    }
//
//    @Test
//    public void testLineReaders_shared_keep_alive() throws IOException {
//        RxShell rxShell = new RxShell(rxProcess);
//        TestObserver<RxShell.Session> sessionObs = rxShell.open().test().awaitCount(1).assertNoErrors();
//
//        RxShell.Session session = sessionObs.values().get(0);
//
//        session.outputLines().test().assertNotComplete().cancel();
//        session.errorLines().test().assertNotComplete().cancel();
//
//        session.outputLines().test().assertNotComplete().cancel();
//        session.errorLines().test().assertNotComplete().cancel();
//
//        await().pollDelay(100, TimeUnit.MILLISECONDS).atMost(1, TimeUnit.SECONDS).until(() -> outputStream.isOpen(), is(true));
//        await().pollDelay(100, TimeUnit.MILLISECONDS).atMost(1, TimeUnit.SECONDS).until(() -> errorStream.isOpen(), is(true));
//
//        session.close().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().assertValue(0);
//
//        await().atMost(1, TimeUnit.SECONDS).until(() -> outputStream.isOpen(), is(false));
//        await().atMost(1, TimeUnit.SECONDS).until(() -> errorStream.isOpen(), is(false));
//    }
//
//    @Test
//    public void testReinit() {
//        RxShell rxShell = new RxShell(rxProcess);
//        RxShell.Session session1 = rxShell.open().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().values().get(0);
//        verify(rxProcess).open();
//        session1.close().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors();
//
//        RxShell.Session session2 = rxShell.open().test().awaitDone(1, TimeUnit.SECONDS).assertNoErrors().values().get(0);
//        assertThat(session2, is(not(session1)));
//        verify(rxProcess, times(2)).open();
//    }
//}
