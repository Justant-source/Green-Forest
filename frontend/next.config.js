/** @type {import('next').NextConfig} */

const imageBaseUrl = process.env.NEXT_PUBLIC_IMAGE_BASE_URL || "http://localhost:8080";
const parsed = new URL(imageBaseUrl);

const nextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: parsed.protocol.replace(":", ""),
        hostname: parsed.hostname,
        ...(parsed.port ? { port: parsed.port } : {}),
      },
    ],
  },
};

module.exports = nextConfig;
