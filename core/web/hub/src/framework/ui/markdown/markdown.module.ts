import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {RichTextEditorComponent} from "framework/ui/markdown/rich-text-editor.component";

@NgModule({
              declarations: [RichTextEditorComponent],
              imports     : [CommonModule],
              exports     : [RichTextEditorComponent]
          })
export class MarkdownModule {}
