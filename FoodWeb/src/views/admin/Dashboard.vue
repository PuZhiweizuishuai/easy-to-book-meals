<template>
  <div>
    <h2 class="text-2xl font-bold mb-6">数据看板</h2>
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <div class="stat bg-base-100 shadow-md rounded-lg">
        <div class="stat-figure text-primary">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-8 w-8"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
        </div>
        <div class="stat-title">今日收入</div>
        <div class="stat-value text-primary">¥12,345</div>
        <div class="stat-desc">较昨日 +12%</div>
      </div>

      <div class="stat bg-base-100 shadow-md rounded-lg">
        <div class="stat-figure text-secondary">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-8 w-8"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
            />
          </svg>
        </div>
        <div class="stat-title">今日订单</div>
        <div class="stat-value text-secondary">156</div>
        <div class="stat-desc">已完成 142 / 进行中 14</div>
      </div>

      <div class="stat bg-base-100 shadow-md rounded-lg">
        <div class="stat-figure text-accent">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-8 w-8"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
            />
          </svg>
        </div>
        <div class="stat-title">菜品数量</div>
        <div class="stat-value text-accent">89</div>
        <div class="stat-desc">上架中 76 / 已下架 13</div>
      </div>

      <div class="stat bg-base-100 shadow-md rounded-lg">
        <div class="stat-figure text-info">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-8 w-8"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"
            />
          </svg>
        </div>
        <div class="stat-title">用户数量</div>
        <div class="stat-value text-info">1,234</div>
        <div class="stat-desc">活跃用户 856</div>
      </div>
    </div>

    <div class="mt-8 grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="card bg-base-100 shadow-md">
        <div class="card-body">
          <h3 class="card-title">最近订单</h3>
          <div class="overflow-x-auto">
            <table class="table table-zebra">
              <thead>
                <tr>
                  <th>订单号</th>
                  <th>金额</th>
                  <th>状态</th>
                  <th>时间</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>ORD-20260425-001</td>
                  <td>¥128.00</td>
                  <td><span class="badge badge-success">已完成</span></td>
                  <td>2026-04-25 14:30</td>
                </tr>
                <tr>
                  <td>ORD-20260425-002</td>
                  <td>¥56.00</td>
                  <td><span class="badge badge-warning">进行中</span></td>
                  <td>2026-04-25 14:25</td>
                </tr>
                <tr>
                  <td>ORD-20260425-003</td>
                  <td>¥88.00</td>
                  <td><span class="badge badge-success">已完成</span></td>
                  <td>2026-04-25 14:20</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-md">
        <div class="card-body">
          <h3 class="card-title">热销菜品 TOP 5</h3>
          <ul class="menu">
            <li>
              <a><span class="badge badge-primary mr-2">1</span>宫保鸡丁</a>
            </li>
            <li>
              <a><span class="badge badge-secondary mr-2">2</span>麻婆豆腐</a>
            </li>
            <li>
              <a><span class="badge badge-accent mr-2">3</span>鱼香肉丝</a>
            </li>
            <li>
              <a><span class="badge badge-info mr-2">4</span>回锅肉</a>
            </li>
            <li>
              <a><span class="badge badge-neutral mr-2">5</span>水煮鱼</a>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup></script>
