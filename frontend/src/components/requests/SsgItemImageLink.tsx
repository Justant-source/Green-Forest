"use client";

import { ReactNode } from "react";

interface Props {
  href?: string | null;
  className?: string;
  children: ReactNode;
}

export default function SsgItemImageLink({ href, className, children }: Props) {
  if (!href) {
    return <div className={className}>{children}</div>;
  }
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      title="SSG에서 상품 보기"
      className={className}
    >
      {children}
    </a>
  );
}
