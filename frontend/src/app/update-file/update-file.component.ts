import {
  Component,
  ViewChild,
  ElementRef,
  Output,
  Input,
  EventEmitter,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Validators, FormBuilder } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MainService } from '../service/main.service';

@Component({
  selector: 'app-update-file',
  templateUrl: './update-file.component.html',
  styleUrls: ['./update-file.component.scss'],
})
export class UpdateFileComponent implements OnChanges {
  fileForm = this.fb.group({
    file: ['', Validators.required],
    tags: ['', Validators.required]
  })

  selectedFiles: File[] = [];
  tagsInput: string = '';
  updateTags: string[] = [];
  uploadInProgress: boolean = false;
  showHint: boolean = false;

  @ViewChild('fileInput') fileInput!: ElementRef;
  @Input() updateItem!: any;
  @Output() closeFileUpdate = new EventEmitter<void>();
  @Output() updateImg = new EventEmitter();
  @Output() closePopup = new EventEmitter();

  constructor(
    private http: HttpClient,
    private fb: FormBuilder,
    private _snackBar: MatSnackBar,
    private mainService: MainService
  ) { }

  ngOnChanges(changes: SimpleChanges): void {
    const { currentValue, previousValue } = changes['updateItem'];

    if (changes['updateItem']) {
      this.updateTags = [...currentValue.tags];
      this.selectedFiles = [currentValue];
    }
  }

  alertBar() {
    this._snackBar.open('You can only select one file.', '', {
      duration: 3000,
      panelClass: 'red-snackbar',
    });
  }

  update(): void {
    const formData = new FormData();
    formData.append('tags', this.updateTags.join(' '));
    this.selectedFiles.forEach((files) => {
      formData.append('file', files);
    });
    this.http.put(`api/files/${this.updateItem.id}`, formData).subscribe(
      res => {
        this.fileForm.reset();
        this.updateImg.emit(res);
        this.closeFileUpdate.emit();
      },
      err => {
        if (err.status === 404) {
          alert('The file you are trying to upload/update does not exist. Please update/upload a correct file.');
        } else if (err.status === 413) {
          alert('Files over 32MB are not supported.');
        }
      }
    );

  }

  checkFileType(fileName: string): string {
    return this.mainService.checkFileType(fileName);
  }
  triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }

  addFiles(event: any): void {
    const maxFileSize = 32 * 1024 * 1024; // 32MB
    const fileList = [...event.target.files];
    const totalFileSize = fileList.map(f => f.size).reduce((p, a) => p + a, 0);

    if (totalFileSize > maxFileSize) {
      alert('Files over 32MB are not supported.');
    } else if (event.target.files.length > 1) {
      this.alertBar();
    } else {
      if (event.target.files.length > 0) {
        this.showHint = false;
        const fileList = [...event.target.files];
        this.selectedFiles = [];
        fileList.forEach((files) => {
          this.selectedFiles.push(files);
        });
      }
    }
  }

  addTags(): void {
    const newTags = this.tagsInput.trim().split(/[,\s]+/).map(s => s.toLowerCase());
    this.updateTags = [...this.updateTags, ...newTags];
    this.tagsInput = '';
  }

  closeModal(event: MouseEvent): void {
    if (confirm('Are you sure you want to exit?')) {
      this.closeFileUpdate.emit();
    }
  }

  stopPropagation(event: MouseEvent): void {
    event.stopPropagation();
  }

  removeTag(index: number): void {
    this.updateTags.splice(index, 1);
  }

}
