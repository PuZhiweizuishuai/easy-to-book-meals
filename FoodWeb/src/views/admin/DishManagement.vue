<template>
  <div>
    <h2 class="text-2xl font-bold mb-6">菜品管理</h2>
    <div class="card bg-base-100 shadow-md">
      <div class="card-body">
        <div class="flex flex-col md:flex-row justify-between gap-4 mb-6">
          <div class="flex flex-col md:flex-row gap-4">
            <input
              type="text"
              placeholder="搜索菜品..."
              class="input input-bordered w-full md:w-64"
            />
            <select class="select select-bordered w-full md:w-48">
              <option value="">所有分类</option>
              <option value="appetizer">凉菜</option>
              <option value="main">热菜</option>
              <option value="soup">汤类</option>
              <option value="drink">饮品</option>
            </select>
          </div>
          <button class="btn btn-primary">添加菜品</button>
        </div>

        <div class="overflow-x-auto">
          <table class="table">
            <thead>
              <tr>
                <th>菜品图片</th>
                <th>菜品名称</th>
                <th>分类</th>
                <th>价格</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><div class="w-12 h-12 bg-base-300 rounded-lg"></div></td>
                <td>宫保鸡丁</td>
                <td>热菜</td>
                <td>¥38.00</td>
                <td><span class="badge badge-success">上架</span></td>
                <td>
                  <button class="btn btn-ghost btn-sm">编辑</button>
                  <button class="btn btn-ghost btn-sm text-error">下架</button>
                </td>
              </tr>
              <tr>
                <td><div class="w-12 h-12 bg-base-300 rounded-lg"></div></td>
                <td>麻婆豆腐</td>
                <td>热菜</td>
                <td>¥28.00</td>
                <td><span class="badge badge-success">上架</span></td>
                <td>
                  <button class="btn btn-ghost btn-sm">编辑</button>
                  <button class="btn btn-ghost btn-sm text-error">下架</button>
                </td>
              </tr>
              <tr>
                <td><div class="w-12 h-12 bg-base-300 rounded-lg"></div></td>
                <td>拍黄瓜</td>
                <td>凉菜</td>
                <td>¥18.00</td>
                <td><span class="badge badge-error">下架</span></td>
                <td>
                  <button class="btn btn-ghost btn-sm">编辑</button>
                  <button class="btn btn-ghost btn-sm text-success">上架</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="flex justify-center mt-4">
          <div class="join">
            <button class="join-item btn">1</button>
            <button class="join-item btn btn-active">2</button>
            <button class="join-item btn">3</button>
            <button class="join-item btn">...</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup></script>
