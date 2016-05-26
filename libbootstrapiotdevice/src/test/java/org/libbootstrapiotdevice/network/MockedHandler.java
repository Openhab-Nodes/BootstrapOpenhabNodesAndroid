package org.libbootstrapiotdevice.network;

import android.os.Handler;
import android.os.Message;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Mocks an android handler with Mockito. All messages are inserted into a queue.
 * Call nextEntry to get the message that would be executed next with a handler.
 */
public class MockedHandler {
    static Handler createMockedHandler(final Map<Long, Message> queue) {
        final Handler handler = Mockito.mock(Handler.class);
        Mockito.when(handler.sendEmptyMessageDelayed(Mockito.anyInt(), Mockito.anyLong())).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Integer what = invocation.getArgumentAt(0, Integer.class);
                Long delay = invocation.getArgumentAt(1, Long.class);
                queue.put(delay, handler.obtainMessage(what, 0, 0, null));
                return null;
            }
        });
        Mockito.when(handler.sendMessage(Mockito.any(Message.class))).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Message msg = invocation.getArgumentAt(0, Message.class);
                Long delay = 0L;
                queue.put(delay, msg);
                return null;
            }
        });
        Mockito.when(handler.sendMessageDelayed(Mockito.any(Message.class), Mockito.anyLong())).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Message msg = invocation.getArgumentAt(0, Message.class);
                Long delay = invocation.getArgumentAt(1, Long.class);
                queue.put(delay, msg);
                return null;
            }
        });
        Mockito.when(handler.obtainMessage(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any())).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Message msg = new Message();
                msg.what = invocation.getArgumentAt(0, Integer.class);
                msg.arg1 = invocation.getArgumentAt(1, Integer.class);
                msg.arg2 = invocation.getArgumentAt(2, Integer.class);
                msg.obj = invocation.getArgumentAt(3, Object.class);
                return msg;
            }
        });
        Mockito.when(handler.obtainMessage(Mockito.anyInt(), Mockito.any())).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Message msg = new Message();
                msg.what = invocation.getArgumentAt(0, Integer.class);
                msg.obj = invocation.getArgumentAt(1, Object.class);
                return msg;
            }
        });
        return handler;
    }

    static Message nextEntry(Set<Map.Entry<Long, Message>> entries, int expectEntries, long expectWaitTime) {
        assertEquals(expectEntries, entries.size());

        Iterator<Map.Entry<Long, Message>> iterator = entries.iterator();
        Map.Entry<Long, Message> entry = iterator.next();
        iterator.remove();

        // The added message should be performed immediately, so wait duration should be 0.
        assertEquals(expectWaitTime, (long) entry.getKey());
        // Check the message type
        assertNotNull(entry.getValue());

        return entry.getValue();
    }
}
