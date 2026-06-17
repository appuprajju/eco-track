FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy application code
COPY . .

# Expose port (adjust if your app uses a different port)
EXPOSE 8080

# Set environment variable for Cloud Run
ENV PORT=8080

# Start the application
CMD ["node", "server.js"]