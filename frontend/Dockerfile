# === Build Stage ===
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install
COPY . .
ARG NEXT_PUBLIC_API_BASE_URL
ARG NEXT_PUBLIC_IMAGE_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_IMAGE_BASE_URL=$NEXT_PUBLIC_IMAGE_BASE_URL
RUN npm run build

# === Run Stage ===
FROM node:20-alpine
WORKDIR /app
COPY --from=build /app/.next ./.next
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/package.json ./
COPY --from=build /app/public ./public
COPY --from=build /app/next.config.js ./
EXPOSE 3000
CMD ["npm", "start"]
