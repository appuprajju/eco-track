# --- Stage 1: Build & Compile TypeScript ---
FROM node:18 AS builder
WORKDIR /usr/src/app

# Copy dependency manifests
COPY package*.json tsconfig.json ./

# Install ALL dependencies (including typescript and compilation tools)
RUN npm install

# Copy your application source code
COPY src/ ./src

# Compile TypeScript to JavaScript
RUN npm run build

# --- Stage 2: Production Execution ---
FROM node:18-slim
WORKDIR /usr/src/app

# Copy dependency manifests
COPY package*.json ./

# Install only production dependencies
RUN npm ci --only=production

# Copy the compiled code from Stage 1
COPY --from=builder /usr/src/app/dist ./dist

# Standard Cloud Run port exposure
EXPOSE 8080

# Execute the compiled server
CMD [ "npm", "start" ]