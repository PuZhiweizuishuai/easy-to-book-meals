<template>
  <div>
    <h2 class="text-2xl font-bold mb-6">订单管理</h2>
    <div class="card bg-base-100 shadow-md">
      <div class="card-body">
        <div class="flex flex-col md:flex-row gap-4 mb-6">
          <input
            type="text"
            placeholder="搜索订单号..."
            class="input input-bordered w-full md:w-64"
          />
          <select class="select select-bordered w-full md:w-48">
            <option value="">所有状态</option>
            <option value="pending">待处理</option>
            <option value="processing">处理中</option>
            <option value="completed">已完成</option>
            <option value="cancelled">已取消</option>
          </select>
          <button class="btn btn-primary">搜索</button>
        </div>

        <div class="overflow-x-auto">
          <table class="table table-zebra">
            <thead>
              <tr>
                <th>订单号</th>
                <th>桌号</th>
                <th>金额</th>
                <th>状态</th>
                <th>下单时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>ORD-20260425-001</td>
                <td>A01</td>
                <td>¥128.00</td>
                <td><span class="badge badge-success">已完成</span></td>
                <td>2026-04-25 14:30</td>
                <td><button class="btn btn-ghost btn-sm">详情</button></td>
              </tr>
              <tr>
                <td>ORD-20260425-002</td>
                <td>B03</td>
                <td>¥56.00</td>
                <td><span class="badge badge-warning">处理中</span></td>
                <td>2026-04-25 14:25</td>
                <td><button class="btn btn-ghost btn-sm">详情</button></td>
              </tr>
              <tr>
                <td>ORD-20260425-003</td>
                <td>C02</td>
                <td>¥88.00</td>
                <td><span class="badge badge-info">待处理</span></td>
                <td>2026-04-25 14:20</td>
                <td><button class="btn btn-ghost btn-sm">详情</button></td>
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
