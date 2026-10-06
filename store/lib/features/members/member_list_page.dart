import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/time_format.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../providers/store_context_provider.dart';
import '../../shared/widgets/app_card.dart';
import '../../shared/widgets/app_network_image.dart';
import '../../shared/widgets/paged_list_view.dart';
import '../../shared/widgets/search_field.dart';
import 'member_detail_page.dart';

/// 会员管理页
///
/// 按手机号搜索门店会员，点击进入详情可查看积分流水、消费记录并调整积分。
class MemberListPage extends ConsumerStatefulWidget {
  const MemberListPage({super.key});

  @override
  ConsumerState<MemberListPage> createState() => _MemberListPageState();
}

class _MemberListPageState extends ConsumerState<MemberListPage> {
  final PagedListController<Membership> _listController =
      PagedListController<Membership>();
  String _keyword = '';

  @override
  Widget build(BuildContext context) {
    final storeId = ref.watch(currentStoreIdProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('会员管理'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded, size: 22),
            onPressed: () => _listController.refresh(),
          ),
        ],
      ),
      body: storeId == null
          ? const Center(child: Text('请先选择门店'))
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(14, 10, 14, 8),
                  child: SearchField(
                    hint: '搜索手机号 / 昵称',
                    onSearch: (value) => setState(() => _keyword = value.trim()),
                  ),
                ),
                Expanded(
                  child: PagedListView<Membership>(
                    controller: _listController,
                    reloadKey: _keyword,
                    fetcher: (page) =>
                        ref.read(membershipApiProvider).list(
                              storeId: storeId,
                              key: _keyword.isEmpty ? null : _keyword,
                              page: page,
                              size: 10,
                            ),
                    emptyTitle: '暂无会员',
                    emptySubtitle: '结算时录入顾客手机号即可自动建档',
                    itemBuilder: (context, member, index) => _MemberCard(
                      member: member,
                      onTap: () => _openDetail(member),
                    ),
                  ),
                ),
              ],
            ),
    );
  }

  Future<void> _openDetail(Membership member) async {
    final storeId = ref.read(currentStoreIdProvider);
    if (storeId == null || member.id == null) return;
    final changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => MemberDetailPage(
          memberId: member.id!,
          storeId: storeId,
        ),
      ),
    );
    if (changed == true) _listController.refresh();
  }
}

/// 会员卡片
class _MemberCard extends StatelessWidget {
  const _MemberCard({required this.member, this.onTap});

  final Membership member;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      onTap: onTap,
      child: Row(
        children: [
          AppNetworkImage(
            url: null,
            size: 44,
            radius: 22,
            placeholderIcon: Symbols.account_circle,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  member.displayName,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  member.phone ?? '未知手机号',
                  style: const TextStyle(
                    fontSize: 12.5,
                    color: AppColors.textSecondary,
                  ),
                ),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Row(
                children: [
                  Icon(Symbols.star,
                      size: 14, color: AppColors.primary),
                  const SizedBox(width: 3),
                  Text(
                    '${member.points}',
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                      color: AppColors.primary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 3),
              Text(
                '用餐 ${member.orderCount} 次',
                style: const TextStyle(
                  fontSize: 11.5,
                  color: AppColors.textHint,
                ),
              ),
              if (member.updateTime != null)
                Text(
                  TimeFormat.monthDay(member.updateTime),
                  style: const TextStyle(
                    fontSize: 11,
                    color: AppColors.textHint,
                  ),
                ),
            ],
          ),
          const SizedBox(width: 6),
          const Icon(Icons.chevron_right, size: 18, color: AppColors.textHint),
        ],
      ),
    );
  }
}
