import express, { Request, Response } from 'express';
import jwt from 'jsonwebtoken';
import bcrypt from 'bcrypt';
import cors from 'cors';
import rateLimit from 'express-rate-limit';

const app = express();
// Cloud Run automatically assigns process.env.PORT. We must listen to it!
const PORT = process.env.PORT || 8080;
const JWT_SECRET = process.env.JWT_SECRET || 'super_secret_organic_key';
const REFRESH_SECRET = process.env.REFRESH_SECRET || 'refresh_clean_air_vortex';

interface User {
  id: number;
  name: string;
  email: string;
  passwordHash: string;
}

const usersLocalStore: User[] = [
  { id: 1, name: 'Eco Pioneer', email: 'citizen@ecotrack.org', passwordHash: '$2b$10$dummyhashhere...' }
];

app.use(cors());
app.use(express.json());

const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100,
  message: 'Too many login inquiries from this IP address, please try again in 15 minutes.'
});

const authenticateToken = (req: any, res: Response, next: any) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) return res.status(401).json({ error: 'Access token required.' });

  jwt.verify(token, JWT_SECRET, (err: any, user: any) => {
    if (err) return res.status(403).json({ error: 'Expired or invalid cryptographic token.' });
    req.user = user;
    next();
  });
};

// --- AUTH HOOKS ---
app.post('/api/v1/auth/signup', authLimiter, async (req: Request, res: Response) => {
  try {
    const { name, email, password } = req.body;
    if (!email || !password || password.length < 8) {
      return res.status(400).json({ error: 'Input Validation Failed: secure password >= 8 chars required.' });
    }

    const salt = await bcrypt.genSalt(10);
    const passwordHash = await bcrypt.hash(password, salt);

    const newUser: User = { id: usersLocalStore.length + 1, name, email, passwordHash };
    usersLocalStore.push(newUser);

    res.status(201).json({ message: 'User registered safely in cloud registry.', userId: newUser.id });
  } catch (error) {
    res.status(500).json({ error: 'Database synchronization issue.' });
  }
});

app.post('/api/v1/auth/login', authLimiter, async (req: Request, res: Response) => {
  try {
    const { email, password } = req.body;
    const user = usersLocalStore.find(u => u.email === email);
    if (!user) return res.status(404).json({ error: 'User registration index not found.' });

    // ⚠️ FIXED: Validate password using bcrypt comparison
    const isPasswordValid = await bcrypt.compare(password, user.passwordHash);
    if (!isPasswordValid) return res.status(401).json({ error: 'Invalid security credentials.' });

    const accessToken = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '15m' });
    const refreshToken = jwt.sign({ id: user.id }, REFRESH_SECRET, { expiresIn: '7d' });

    res.json({
      accessToken,
      refreshToken,
      profile: { id: user.id, name: user.name, email: user.email }
    });
  } catch (error) {
    res.status(500).json({ error: 'Internal verification failure.' });
  }
});

// --- RESOURCE HARVESTING HOOKS ---
app.post('/api/v1/carbon/sync', authenticateToken, (req: any, res: Response) => {
  const { logs } = req.body;
  res.json({
    status: 'success',
    timestamp: Date.now(),
    mergedCount: logs ? logs.length : 0,
    syncHash: '0x8f2d1e90'
  });
});

app.listen(PORT, () => {
  console.log(`[EcoTrack AI Server] running cleanly on port ${PORT}`);
});