# --- Stage 1: Build & Compile TypeScript ---
FROM node:18-alpine AS builder
WORKDIR /usr/src/app
COPY package*.json tsconfig.json ./
RUN npm install
# Copy the src folder into the builder container
COPY src/ ./src
RUN npm run build

# --- Stage 2: Production Execution ---
FROM node:18-alpine
WORKDIR /usr/src/app
COPY package*.json ./
RUN npm ci --only=production
COPY --from=builder /usr/src/app/dist ./dist

EXPOSE 8080
CMD [ "npm", "start" ]