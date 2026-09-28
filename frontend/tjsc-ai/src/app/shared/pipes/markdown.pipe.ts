import { inject, Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';

const CNJ_REGEX = /(\d{7}-\d{2}\.\d{4}\.\d\.\d{2}\.\d{4})/g;

@Pipe({ name: 'markdown' })
export class MarkdownPipe implements PipeTransform {
  private readonly sanitizer = inject(DomSanitizer);

  transform(value: string, streaming = false): SafeHtml {
    if (!value) return '';
    if (streaming) {
      const escaped = value
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
      return this.sanitizer.bypassSecurityTrustHtml(
        `<span style="white-space:pre-wrap">${escaped}</span>`
      );
    }
    const html = (marked.parse(value) as string)
      .replace(CNJ_REGEX, '<span class="processo-ref" data-numero="$1">$1</span>');
    return this.sanitizer.bypassSecurityTrustHtml(html);
  }
}
