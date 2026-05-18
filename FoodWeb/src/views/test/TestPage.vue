<template>
  <div class="container mx-auto p-8">
    <div class="max-w-xl mx-auto space-y-8">
      
      <h1 class="text-3xl font-bold text-center">图片上传示例</h1>

      <!-- 单图上传示例：封面图 (16:9) -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title">上传封面图 (16:9)</h2>
          <p class="text-sm text-base-content/70">选择图片后弹窗手动裁剪，确认后点击上传按钮</p>
          <ImageUpload 
            aspect-ratio="16:9" 
            :allow-multiple="false" 
            @upload-success="handleCoverSuccess"
            @upload-error="handleUploadError"
          />
        </div>
      </div>

      <!-- 多图上传示例：商品图 (1:1) -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title">上传商品图 (1:1 多图)</h2>
          <ImageUpload 
            aspect-ratio="1:1" 
            :allow-multiple="true" 
            @upload-success="handleProductSuccess"
            @upload-error="handleUploadError"
          />
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import ImageUpload from '@/components/upload/ImageUpload.vue'; // 路径请自行修改

const handleCoverSuccess = (data) => {
  console.log('封面图上传成功:', data);
  // 处理服务器返回数据
};

const handleProductSuccess = (data) => {
  console.log('商品图上传成功:', data);
  // 处理服务器返回数据
};

const handleUploadError = (error) => {
  console.error('上传出错:', error);
};
</script>