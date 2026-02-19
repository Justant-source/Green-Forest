# 프론트엔드 이미지 압축

## Context
S3에 업로드되는 이미지 용량이 큼. 업로드 전 브라우저에서 `browser-image-compression` 라이브러리로 압축하여 S3 저장 용량과 업로드 시간을 줄인다.

## 수정 파일

### 1. `frontend/package.json`
- `browser-image-compression` 의존성 추가

### 2. `frontend/src/lib/imageCompression.ts` (신규)
- 압축 유틸 함수: `compressImage(file: File): Promise<File>`
- 설정: maxSizeMB=1, maxWidthOrHeight=1920, useWebWorker=true

### 3. `frontend/src/app/posts/new/page.tsx`
- `handleImageChange`에서 파일 선택 후 각 파일에 `compressImage()` 호출
- 압축된 파일을 `imageFiles` state에 저장

### 4. `frontend/src/app/posts/[id]/edit/page.tsx`
- 동일하게 `handleImageChange`에서 `compressImage()` 호출

## 검증
- 게시글 작성 시 큰 이미지(5MB+) 업로드 → S3에 1MB 이하로 저장되는지 확인
- 이미지 프리뷰가 정상 표시되는지 확인
- 여러 장 업로드 시 모두 압축 적용되는지 확인