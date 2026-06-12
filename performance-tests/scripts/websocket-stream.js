import ws from 'k6/ws';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const WS_URL = __ENV.WS_URL || 'ws://localhost:8080/ws/logs';

const messagesReceived = new Counter('ws_messages_received');

export const options = {
  vus: 20,
  duration: '60s',
  thresholds: {
    ws_session_duration: ['p(95)<5000'],
    ws_messages_received: ['count>100'],
  },
};

export default function () {
  const res = ws.connect(WS_URL, {}, function (socket) {
    socket.on('open', function () {
      socket.send(JSON.stringify({ action: 'SUBSCRIBE', channel: 'logs' }));
    });

    socket.on('message', function (message) {
      messagesReceived.add(1);
      check(message, {
        'message is non-empty': (m) => m !== undefined && m.length > 0,
      });
    });

    socket.setTimeout(function () {
      socket.close();
    }, 10000);
  });

  check(res, {
    'connection upgraded (status 101)': (r) => r && r.status === 101,
  });
}
