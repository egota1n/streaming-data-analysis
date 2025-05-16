const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const { Kafka } = require('kafkajs');
const mysql = require('mysql2/promise');

const app = express();
const server = http.createServer(app);
const io = new Server(server);

app.use(express.static('public'));

let db;
const kafka = new Kafka({
  clientId: 'earthquake-web-client',
  brokers: ['kafka:9092'],
});

app.get('/api/earthquakes', async (req, res) => {
  try {
    const [earthquakes] = await db.query(
      'SELECT * FROM earthquakes ORDER BY timestamp DESC LIMIT 50'
    );
    res.json(earthquakes);
  } catch (err) {
    console.error('Failed to fetch earthquakes:', err);
    res.status(500).json({ error: 'Database error' });
  }
});

app.get('/api/stats', async (req, res) => {
  try {
    const [stats] = await db.query(
      'SELECT * FROM hourly_stats ORDER BY window_end DESC LIMIT 24'
    );
    res.json(stats);
  } catch (err) {
    console.error('Failed to fetch stats:', err);
    res.status(500).json({ error: 'Database error' });
  }
});

async function initializeConnections() {
  try {
    db = await mysql.createConnection({
      host: 'host.docker.internal',
      user: 'root',
      password: '',
      database: 'earthquake_db',
    });
    console.log('✅ Connected to MySQL');

    const consumer = kafka.consumer({ groupId: 'earthquake-ui-group' });
    await consumer.connect();
    console.log('✅ Connected to Kafka');

    await consumer.subscribe({
      topic: 'earthquakes-strong',
      fromBeginning: false
    });
    await consumer.subscribe({
      topic: 'earthquakes-stats-hourly',
      fromBeginning: false
    });

    await consumer.run({
      eachMessage: async ({ topic, message }) => {
        const data = message.value.toString();
        console.log(`📩 Received message on topic ${topic}`);

        try {
          const parsed = JSON.parse(data);

          if (topic === 'earthquakes-strong') {
            await db.execute(
              `INSERT INTO earthquakes
              (location, magnitude, timestamp, latitude, longitude)
              VALUES (?, ?, ?, ?, ?)`,
              [
                parsed.location,
                parsed.magnitude,
                parsed.timestamp,
                parsed.latitude,
                parsed.longitude
              ]
            );
            io.emit('earthquake', data);
          } else if (topic === 'earthquakes-stats-hourly') {
            await db.execute(
              `INSERT INTO hourly_stats
              (window_start, window_end, count)
              VALUES (?, ?, ?)`,
              [
                parsed.window_start,
                parsed.window_end,
                parsed.count
              ]
            );
            io.emit('stats-hourly', data);
          }
        } catch (e) {
          console.error('❌ Failed to process message:', e);
        }
      }
    });
  } catch (err) {
    console.error('❌ Initialization error:', err);
    process.exit(1);
  }
}

initializeConnections().catch(console.error);

server.listen(3015, () => {
  console.log('🚀 Server is running on http://localhost:3015');
});