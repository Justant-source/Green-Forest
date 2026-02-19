import imageCompression from "browser-image-compression";

export async function compressImage(file: File): Promise<File> {
  if (file.size <= 1 * 1024 * 1024) {
    return file;
  }

  const options = {
    maxSizeMB: 1,
    maxWidthOrHeight: 1920,
    useWebWorker: true,
  };

  const compressed = await imageCompression(file, options);
  return new File([compressed], file.name, { type: file.type || compressed.type });
}
