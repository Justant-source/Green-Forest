import imageCompression from "browser-image-compression";

/**
 * Photo Bingo 전용 이미지 압축 (1600px 장축, JPEG q≈85)
 */
export async function compressBingoImage(file: File): Promise<File> {
  try {
    const compressed = await imageCompression(file, {
      maxSizeMB: 2,
      maxWidthOrHeight: 1600,
      initialQuality: 0.85,
      useWebWorker: true,
      fileType: "image/jpeg",
    });
    const fileName = file.name.replace(/\.[^.]+$/, "") + ".jpg";
    return new File([compressed], fileName, { type: "image/jpeg" });
  } catch {
    return file;
  }
}
